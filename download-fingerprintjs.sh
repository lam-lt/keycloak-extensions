#!/bin/bash

# Create directory if it doesn't exist
mkdir -p themes/mytheme/login/resources/js

# Download FingerprintJS v4
curl -L https://github.com/fingerprintjs/fingerprintjs/releases/download/v4.2.1/fpjs-pro.umd.min.js -o themes/mytheme/login/resources/js/fingerprintjs.min.js

echo "FingerprintJS has been downloaded to themes/mytheme/login/resources/js/fingerprintjs.min.js"
