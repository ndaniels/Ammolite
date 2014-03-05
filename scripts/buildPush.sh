#!/bin/bash

ant
echo $1
git add -A
git commit -a -m "$1"
git push
