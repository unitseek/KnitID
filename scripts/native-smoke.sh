#!/usr/bin/env bash
set -euo pipefail
mvn -B -ntp -pl knitid-samples -am -Pnative -DskipTests package
./knitid-samples/target/knitid-samples
