#!/bin/bash

podman compose down
status=$?

if [ $status -eq 0 ]; then
    echo "Successfully uninstalled"
    exit 0
else
    echo "Uninstall failed"
    exit 1
fi
