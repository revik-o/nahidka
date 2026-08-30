#!/bin/bash
cd "$(dirname "$0")/.."
mise exec -- make -C back-end test
mise exec -- make -C client test
