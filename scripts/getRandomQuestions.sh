#!/bin/bash

jq -j '.[] | .id, " ", .question, "\n"' < ./nfL6.json | gshuf | head -10
