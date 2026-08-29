#!/bin/bash
cd "$(dirname "$0")/.."
mise exec -- make -C back-end build
mise exec -- make -C client build
