#!/bin/bash
cd "$(dirname "$0")/../client"
mise exec -- make build-android
