# MagicWord Online Library

This repository hosts community shared libraries for MagicWord.

## Structure

Each upload is stored in a directory named with a Timestamp.
- `/{timestamp}/info.json`: Metadata (Name, Description, Author)
- `/{timestamp}/library.json`: The actual library content

## Automation

A GitHub Action automatically scans all folders and generates `index.json` in the root.
Client apps fetch `index.json` to list available libraries.
