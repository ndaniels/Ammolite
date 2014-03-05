#!/bin/bash

ant
echo $1
git commit -a -m "$1"
git push
