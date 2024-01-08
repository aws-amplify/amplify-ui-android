#!/bin/bash

set -x
set -e

# This bucket contains a collection of config files that are used by the
# integration tests. The configuration files contain sensitive
# tokens/credentials/identifiers, so are not published publicly.
readonly config_bucket=$1

readonly config_files=(
    # Liveness
    "liveness/src/androidTest/res/raw/amplifyconfiguration.json"
)

# Set up output path
declare -r dest_dir=$HOME/.aws-amplify/amplify-android
mkdir -p "$dest_dir"

# Download remote files into a local directory outside of the project.
for config_file in ${config_files[@]}; do
    aws s3 cp "s3://$config_bucket/$config_file" "$dest_dir/$config_file" &
done
wait

# Create a symlink for each configuration file.
for config_file in ${config_files[@]}; do
    mkdir -p "$(dirname "$config_file")"
    ln -s "$dest_dir/$config_file" "$config_file" &
done
wait
