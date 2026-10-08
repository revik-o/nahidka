package org.orev.nahidka.window.nativeapi

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Structure
import com.sun.jna.platform.unix.X11
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.NativeLongByReference
import com.sun.jna.ptr.PointerByReference
import java.awt.EventQueue
import java.awt.Window
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil

@Structure.FieldOrder("hi", "lo")
internal class XSyncValue() : Structure(), Structure.ByValue {
    @JvmField var hi: Int = 0
    @JvmField var lo: Int = 0

    constructor(value: Long) : this() {
        hi = (value ushr 32).toInt()
        lo = value.toInt()
    }
}

internal interface XSyncExtension : Library {
    fun XSyncInitialize(display: X11.Display, major: IntByReference, minor: IntByReference): Int
    fun XSyncCreateCounter(display: X11.Display, value: XSyncValue): NativeLong
    fun XSyncSetCounter(display: X11.Display, counter: NativeLong, value: XSyncValue): Int
    fun XSyncDestroyCounter(display: X11.Display, counter: NativeLong): Int
}

internal class X11ResizeSync(
    private val window: Window,
    private val requestRepaint: () -> Unit,
) : AutoCloseable {
    class RepaintToken internal constructor(internal val value: Long, internal val width: Int, internal val height: Int)

    private val access = AwtX11Access()
    private val x11 = X11.INSTANCE
    private val sync by lazy { Native.load("Xext", XSyncExtension::class.java) }
    private val toolkit = Class.forName("sun.awt.X11.XToolkit")
    private val dispatcherType = Class.forName("sun.awt.X11.XEventDispatcher")
    private val eventType = Class.forName("sun.awt.X11.XEvent")
    private val clientMessageType = Class.forName("sun.awt.X11.XClientMessageEvent")
    private val configureType = Class.forName("sun.awt.X11.XConfigureEvent")
    private val addDispatcher = accessible(toolkit, "addEventDispatcher", Long::class.javaPrimitiveType!!, dispatcherType)
    private val removeDispatcher = accessible(toolkit, "removeEventDispatcher", Long::class.javaPrimitiveType!!, dispatcherType)
    private val sunToolkit = Class.forName("sun.awt.SunToolkit")
    private val lock = accessible(sunToolkit, "awtLock")
    private val unlock = accessible(sunToolkit, "awtUnlock")
    private val getType = accessible(eventType, "get_type")
    private val getClientMessage = accessible(eventType, "get_xclient")
    private val getMessageType = accessible(clientMessageType, "get_message_type")
    private val getFormat = accessible(clientMessageType, "get_format")
    private val getData = accessible(clientMessageType, "get_data", Int::class.javaPrimitiveType!!)
    private val getConfigure = accessible(eventType, "get_xconfigure")
    private val getWidth = accessible(configureType, "get_width")
    private val getHeight = accessible(configureType, "get_height")
    private val configuredRequest = AtomicReference<RepaintToken?>(null)
    private val repaintQueued = AtomicBoolean(false)
    private val requestCount = AtomicLong()
    private val completedCount = AtomicLong()
    private val tracing = java.lang.Boolean.getBoolean("nahidka.window.resizeSync.trace")
    private var connection: X11.Display? = null
    private var clientId = 0L
    private var counter = NativeLong(0)
    private var protocolsAtom = X11.Atom(0)
    private var requestAtom = X11.Atom(0)
    private var counterAtom = X11.Atom(0)
    private var dispatcher: Any? = null
    private var waitingValue: Long? = null
    private var followupToken: RepaintToken? = null
    @Volatile private var active = false
    @Volatile private var closed = false

    val requestedFrames get() = requestCount.get()
    val acknowledgedFrames get() = completedCount.get()

    fun start(): Boolean {
        check(EventQueue.isDispatchThread())
        if (closed) {
            if (tracing) System.err.println("Nahidka resize sync: disabled, integration already closed")
            return false
        }
        if (active) return true
        return access.withWindow(window) { display, client ->
            val request = x11.XInternAtom(display, "_NET_WM_SYNC_REQUEST", false)
            val major = IntByReference()
            val minor = IntByReference()
            if (sync.XSyncInitialize(display, major, minor) == 0) {
                if (tracing) System.err.println("Nahidka resize sync: disabled, XSync extension unavailable")
                return@withWindow false
            }

            val protocols = x11.XInternAtom(display, "WM_PROTOCOLS", false)
            val existing = property(display, client, protocols)
            val counterProperty = x11.XInternAtom(display, "_NET_WM_SYNC_REQUEST_COUNTER", false)
            if (request.toLong() in existing || property(display, client, counterProperty).isNotEmpty()) {
                if (tracing) System.err.println("Nahidka resize sync: disabled, another resize synchronization handler is installed")
                return@withWindow false
            }

            connection = display
            clientId = client.toLong()
            protocolsAtom = protocols
            requestAtom = request
            counterAtom = counterProperty
            counter = sync.XSyncCreateCounter(display, XSyncValue(0))
            check(counter.toLong() != 0L) { "Cannot create X11 resize synchronization counter" }

            try {
                dispatcher = Proxy.newProxyInstance(dispatcherType.classLoader, arrayOf(dispatcherType)) { proxy, method, arguments ->
                    when (method.name) {
                        "dispatchEvent" -> {
                            if (active && !closed) dispatch(checkNotNull(arguments?.get(0)))
                            null
                        }
                        "equals" -> proxy === arguments?.get(0)
                        "hashCode" -> System.identityHashCode(proxy)
                        "toString" -> "Nahidka X11 resize synchronization dispatcher"
                        else -> null
                    }
                }
                addDispatcher.invoke(null, clientId, dispatcher)
                active = true
                setProperty(display, client, counterAtom, X11.XA_CARDINAL, listOf(counter.toLong()))
                setProperty(display, client, protocolsAtom, X11.XA_ATOM, existing + request.toLong())
                x11.XFlush(display)
                if (tracing) System.err.println("Nahidka resize sync: enabled counter=${counter.toLong()} extension=${major.value}.${minor.value}")
                true
            } catch (failure: Throwable) {
                close()
                throw failure
            }
        }
    }

    private fun dispatch(event: Any) {
        when (getType.invoke(event) as Int) {
            X11.ClientMessage -> {
                val message = getClientMessage.invoke(event)
                if (getMessageType.invoke(message) as Long != protocolsAtom.toLong() ||
                    getFormat.invoke(message) as Int != 32 ||
                    getData.invoke(message, 0) as Long != requestAtom.toLong()) return
                val low = (getData.invoke(message, 2) as Long) and 0xffffffffL
                val high = (getData.invoke(message, 3) as Long) and 0xffffffffL
                waitingValue = (high shl 32) or low
                requestCount.incrementAndGet()
            }
            X11.ConfigureNotify -> {
                val value = waitingValue ?: return
                waitingValue = null
                val configure = getConfigure.invoke(event)
                val request = RepaintToken(value, getWidth.invoke(configure) as Int, getHeight.invoke(configure) as Int)
                configuredRequest.set(request)
                if (tracing) System.err.println("Nahidka resize sync: request=${request.value} size=${request.width}x${request.height}")
                queueRepaint()
            }
        }
    }

    private fun queueRepaint() {
        if (!repaintQueued.compareAndSet(false, true)) return
        EventQueue.invokeLater {
            repaintQueued.set(false)
            if (active && !closed && window.isShowing && configuredRequest.get() != null) requestRepaint()
        }
    }

    fun beginRepaint(): RepaintToken? {
        check(EventQueue.isDispatchThread())
        return if (active && !closed) configuredRequest.get() else null
    }

    fun repaintCompleted(request: RepaintToken?) {
        check(EventQueue.isDispatchThread())
        if (!active || closed || !window.isDisplayable) return
        withConnection { display ->
            if (request == null || configuredRequest.get() !== request) {
                retryRepaint()
                return@withConnection
            }
            val scale = window.graphicsConfiguration.defaultTransform
            if (window.width != ceil(request.width / scale.scaleX - 0.5).toInt() ||
                window.height != ceil(request.height / scale.scaleY - 0.5).toInt()) {
                retryRepaint()
                return@withConnection
            }
            check(sync.XSyncSetCounter(display, counter, XSyncValue(request.value)) != 0) {
                "Cannot acknowledge X11 resize repaint"
            }
            configuredRequest.compareAndSet(request, null)
            followupToken = null
            completedCount.incrementAndGet()
            x11.XFlush(display)
            if (tracing) System.err.println("Nahidka resize sync: painted=${request.value} size=${request.width}x${request.height}")
        }
    }

    private fun retryRepaint() {
        val request = configuredRequest.get() ?: return
        if (followupToken === request) return
        followupToken = request
        queueRepaint()
    }

    override fun close() {
        check(EventQueue.isDispatchThread())
        if (closed) return
        closed = true
        active = false
        configuredRequest.set(null)
        followupToken = null
        withConnection { display ->
            dispatcher?.let { removeDispatcher.invoke(null, clientId, it) }
            dispatcher = null
            if (counter.toLong() != 0L) {
                if (window.isDisplayable) {
                    val client = X11.Window(clientId)
                    if (property(display, client, counterAtom) == listOf(counter.toLong())) {
                        val protocols = property(display, client, protocolsAtom).filter { it != requestAtom.toLong() }
                        setProperty(display, client, protocolsAtom, X11.XA_ATOM, protocols)
                        x11.XDeleteProperty(display, client, counterAtom)
                    }
                }
                sync.XSyncDestroyCounter(display, counter)
                counter = NativeLong(0)
                x11.XFlush(display)
            }
        }
        connection = null
    }

    private fun <T> withConnection(block: (X11.Display) -> T): T? {
        val display = connection ?: return null
        lock.invoke(null)
        try {
            return block(display)
        } finally {
            unlock.invoke(null)
        }
    }

    private fun property(display: X11.Display, client: X11.Window, atom: X11.Atom): List<Long> {
        val type = X11.AtomByReference()
        val format = IntByReference()
        val count = NativeLongByReference()
        val remaining = NativeLongByReference()
        val value = PointerByReference()
        val status = x11.XGetWindowProperty(display, client, atom, NativeLong(0), NativeLong(4096),
            false, X11.Atom(X11.AnyPropertyType.toLong()), type, format, count, remaining, value)
        val buffer = value.value
        try {
            if (status != 0 || buffer == null || format.value != 32) return emptyList()
            return List(count.value.toInt()) { buffer.getNativeLong(it.toLong() * NativeLong.SIZE).toLong() }
        } finally {
            if (buffer != null) x11.XFree(buffer)
        }
    }

    private fun setProperty(display: X11.Display, client: X11.Window, atom: X11.Atom,
                            type: X11.Atom, values: List<Long>) {
        if (values.isEmpty()) {
            x11.XDeleteProperty(display, client, atom)
            return
        }
        Memory(values.size.toLong() * NativeLong.SIZE).use { memory ->
            values.forEachIndexed { index, value -> memory.setNativeLong(index.toLong() * NativeLong.SIZE, NativeLong(value)) }
            x11.XChangeProperty(display, client, atom, type, 32, X11.PropModeReplace, memory, values.size)
        }
    }

    private fun accessible(owner: Class<*>, name: String, vararg types: Class<*>): Method =
        owner.getDeclaredMethod(name, *types).apply {
            check(trySetAccessible()) { "Missing module access for ${owner.name}.$name" }
        }
}
