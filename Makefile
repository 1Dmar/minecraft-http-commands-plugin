# Makefile for HTTP Commands Plugin
# Usage: make set-version VERSION=1.0.2

# Default target
.PHONY: help
help:
	@echo "ProMcScure plugin - Makefile"
	@echo ""
	@echo "Available targets:"
	@echo "  set-version VERSION=x.x.x   - Update version in build.gradle, plugin.yml, and openapi.yaml"
	@echo "  build                       - Build the plugin JAR"
	@echo "  build-docs                 - Build the plugin and generate API documentation"
	@echo "  clean                      - Clean build artifacts"

# Update version in all configuration files
.PHONY: set-version
set-version:
	@if [ -z "$(VERSION)" ]; then \
		echo "Usage: make set-version VERSION=1.0.2"; \
		exit 1; \
	fi
	@echo "Updating version to $(VERSION)..."
	@# Update build.gradle
	@sed -i '' "s/version = '.*'/version = '$(VERSION)'/" build.gradle
	@# Update plugin.yml (only the version line, not api-version)
	@sed -i '' "s/^version: .*/version: $(VERSION)/" src/main/resources/plugin.yml
	@# Update openapi.yaml (preserve the 2-space indentation under info:)
	@sed -i '' "s/^  version: .*/  version: $(VERSION)/" src/main/api/openapi.yaml
	@echo "Version updated to $(VERSION) in:"
	@echo "  - build.gradle"
	@echo "  - src/main/resources/plugin.yml"
	@echo "  - src/main/api/openapi.yaml"

# Build the plugin
.PHONY: build
build:
	./gradlew build

# Build and generate API docs
.PHONY: build-docs
build-docs:
	./gradlew build openApiGenerate

# Clean build artifacts
.PHONY: clean
clean:
	./gradlew clean
