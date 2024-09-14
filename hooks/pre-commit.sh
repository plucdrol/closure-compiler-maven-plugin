#!/usr/bin/env bash

stagedFiles=$(git diff --staged --name-only)

mvn spotless:apply || exit 1

for file in $stagedFiles; do
  if test -f "$file"; then
    git add $file
  fi
done
