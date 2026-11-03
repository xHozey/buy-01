#!/bin/sh
set -eu

namespace=mr-jenk
release="build-${BUILD_NUMBER:-$(date +%Y%m%d%H%M%S)}"
services='server user product media gateway frontend'

rollback() {
    echo 'Deployment failed. Restoring previous images.'
    export IMAGE_TAG=previous
    docker compose -p backend -f backend/docker-compose.yml up -d --no-build || true
    docker compose -p frontend -f frontend/docker-compose.yaml up -d --no-build || true
}

for service in $services; do
    if docker image inspect "$namespace/$service:latest" >/dev/null 2>&1; then
        docker tag "$namespace/$service:latest" "$namespace/$service:previous"
    fi
done

if ! (
    export IMAGE_NAMESPACE="$namespace"
    export IMAGE_TAG="$release"

    docker compose -p backend -f backend/docker-compose.yml build
    docker compose -p frontend -f frontend/docker-compose.yaml build

    for service in $services; do
        docker tag "$namespace/$service:$release" "$namespace/$service:latest"
    done

    export IMAGE_TAG=latest
    docker compose -p backend -f backend/docker-compose.yml up -d --no-build
    docker compose -p frontend -f frontend/docker-compose.yaml up -d --no-build
); then
    rollback
    exit 1
fi

echo "Deployment completed: $release"