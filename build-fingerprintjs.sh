#!/bin/bash

# Create a temporary directory
mkdir -p temp_build
cd temp_build

# Initialize npm and install FingerprintJS
npm init -y
npm install @fingerprintjs/fingerprintjs

# Create a simple script to bundle
echo "import FingerprintJS from '@fingerprintjs/fingerprintjs';
window.FingerprintJS = FingerprintJS;" > index.js

# Install and use esbuild for bundling (much faster than webpack)
npm install --save-dev esbuild
npx esbuild index.js --bundle --outfile=../themes/mytheme/login/resources/js/fingerprintjs.min.js --minify

# Clean up
cd ..
rm -rf temp_build

echo "FingerprintJS has been built and saved to themes/mytheme/login/resources/js/fingerprintjs.min.js"
