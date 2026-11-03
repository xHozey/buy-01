#!/bin/sh
set -eu

engine="${CONTAINER_ENGINE:-docker}"
namespace="${IMAGE_NAMESPACE:-mr-jenk}"
release="${RELEASE_TAG:-build-${BUILD_NUMBER:-$(date +%Y%m%d%H%M%S)}}"
backend_project="${BACKEND_COMPOSE_PROJECT:-mr-jenk-backend}"
frontend_project="${FRONTEND_COMPOSE_PROJECT:-mr-jenk-frontend}"

compose() {
    "$engine" compose "$@"
}

backend_compose() {
    compose -p "$backend_project" -f backend/docker-compose.yml "$@"
}

frontend_compose() {
    compose -p "$frontend_project" -f frontend/docker-compose.yaml "$@"
}

image_exists() {
    "$engine" image inspect "$1" >/dev/null 2>&1
}

images='server user product media gateway frontend'

rollback() {
    echo 'Deployment failed; rolling back to previous images.'
    backend_compose down --remove-orphans || true
    frontend_compose down --remove-orphans || true

    for service in $images; do
        image="$namespace/$service:previous"
        if ! image_exists "$image"; then
            echo "No complete previous image set exists; services remain stopped." >&2
            return 0
        fi
    done

    export IMAGE_NAMESPACE="$namespace"
    export IMAGE_TAG=previous
    backend_compose up -d --no-build --remove-orphans
    frontend_compose up -d --no-build --remove-orphans
    echo 'Rollback completed.'
}

for service in $images; do
    image="$namespace/$service"
    if image_exists "$image:latest"; then
        "$engine" tag "$image:latest" "$image:previous"
    fi
done

if ! (
    export IMAGE_NAMESPACE="$namespace"
    export IMAGE_TAG="$release"

    backend_compose build
    frontend_compose build

    for service in $images; do
        "$engine" tag "$namespace/$service:$release" "$namespace/$service:latest"
    done

    export IMAGE_TAG=latest
    backend_compose up -d --no-build --remove-orphans
    frontend_compose up -d --no-build --remove-orphans

    for service in server user product media gateway mongodb broker; do
        container=$(backend_compose ps -q "$service")
        [ -n "$container" ]
        [ "$($engine inspect -f '{{.State.Status}}' "$container")" = running ]
    done

    container=$(frontend_compose ps -q web)
    [ -n "$container" ]
    [ "$($engine inspect -f '{{.State.Status}}' "$container")" = running ]
); then
    rollback
    exit 1
fi

echo "Deployment completed with image tag $release."