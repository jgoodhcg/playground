# Playground

A personal coding playground for Clojure, ClojureScript, and Babashka experiments.

## Projects

This repository contains various small applications and experiments:

## Prerequisites

- Clojure
- Node.js
- Babashka (for CLI scripts)

## Running Projects

### Shadow-cljs
Shadow-CLJS is used for ClojureScript builds:

```bash
# Install dependencies
npm install

# Run specific project
npx shadow-cljs watch memento-mori
npx shadow-cljs watch nivo-fat-secret
npx shadow-cljs watch quil-cljs-test-render
```

### Babashka 

Example of running a Babashka script:

```bash
bb text_file_aggregator.clj --directory <dir> --output <output-file>
```
### JVM Clojure

I use Cider and Emacs and `cider-jack-in-clj` to run these projects
