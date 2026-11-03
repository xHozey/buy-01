#!/bin/sh
set -eu

certificate_dir=/etc/nginx/certs
certificate_file="$certificate_dir/frontend.crt"
key_file="$certificate_dir/frontend.key"

mkdir -p "$certificate_dir"

if [ ! -s "$certificate_file" ] || [ ! -s "$key_file" ]; then
    openssl req -x509 -nodes -newkey rsa:2048 \
        -keyout "$key_file" \
        -out "$certificate_file" \
        -days 365 \
        -subj "/CN=localhost" \
        -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
    chmod 600 "$key_file"
fi