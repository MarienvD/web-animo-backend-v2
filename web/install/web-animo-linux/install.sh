#!/bin/bash

podman compose up -d
status=$?

if [ $status -eq 0 ]; then
    echo "Successfully installed"
    exit 0
else
    echo "Installation failed"
    exit 1
fi
