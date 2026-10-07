
# ----------------------------------------------------------------------
# Swingy — self-sufficient build
#
# Fresh clone:
#   make            # download Maven, build the jar
#   make mvn ARGS="clean package"    # run Maven directly
#   make run         # launch console mode
#   make env         # print how to activate mvn in your shell
#   make clean       # remove tools/ and target/
# ----------------------------------------------------------------------

MAVEN_VERSION := 3.9.6
MAVEN_NAME    := apache-maven-$(MAVEN_VERSION)
MAVEN_URL     := https://archive.apache.org/dist/maven/maven-3/$(MAVEN_VERSION)/binaries/$(MAVEN_NAME)-bin.tar.gz
MAVEN_TARBALL := tools/$(MAVEN_NAME)-bin.tar.gz

TOOLS_DIR     := tools
MAVEN_HOME    := $(TOOLS_DIR)/$(MAVEN_NAME)
MAVEN_BIN     := $(MAVEN_HOME)/bin
MVN           := $(MAVEN_BIN)/mvn
MAVEN_ENV     := $(TOOLS_DIR)/maven-env.sh

JAR           := target/swingy.jar

.PHONY: all install build run env clean distclean

# ---- Default target --------------------------------------------------
all: build

# ---- Full setup + build ---------------------------------------------
install: $(MVN) $(MAVEN_ENV)
build: $(JAR)

$(JAR): $(MVN) pom.xml
	@$(MVN) -q clean package
	@echo "✔ Built $(JAR)"

# ---- Download Maven --------------------------------------------------
$(TOOLS_DIR):
	@mkdir -p $(TOOLS_DIR)

$(MAVEN_TARBALL): | $(TOOLS_DIR)
	@echo "→ Downloading Maven $(MAVEN_VERSION)..."
	@curl -fL $(MAVEN_URL) -o $(MAVEN_TARBALL).tmp
	@if ! file -b $(MAVEN_TARBALL).tmp | grep -q 'gzip compressed'; then \
		echo "ERROR: download is not a gzip tarball (mirror returned an error page)." >&2; \
		echo "       Try again, or set MAVEN_URL to https://archive.apache.org/dist/..." >&2; \
		rm -f $(MAVEN_TARBALL).tmp; \
		exit 1; \
	fi
	@mv $(MAVEN_TARBALL).tmp $(MAVEN_TARBALL)
	@echo "✔ Downloaded $(MAVEN_TARBALL)"

# ---- Extract Maven ---------------------------------------------------
$(MVN): $(MAVEN_TARBALL)
	@echo "→ Extracting Maven..."
	@tar -xzf $(MAVEN_TARBALL) -C $(TOOLS_DIR)
	@echo "✔ Maven installed at $(MAVEN_HOME)"

# ---- Write activation script ----------------------------------------
$(MAVEN_ENV): $(MVN)
	@printf 'export PATH="%s:$$PATH"\n' "$(abspath $(MAVEN_BIN))" > $(MAVEN_ENV)
	@echo "✔ Wrote $(MAVEN_ENV)"

# ---- Convenience targets --------------------------------------------
mvn: $(MVN)
	@$(MVN) $(ARGS)

run: $(JAR)
	@java -jar $(JAR) console

env: $(MAVEN_ENV)
	@echo "To use mvn in this shell, run:"
	@echo "    source $(MAVEN_ENV)"

# ---- Cleanup ---------------------------------------------------------
clean:
	@echo "→ Removing $(MAVEN_HOME) and $(MAVEN_TARBALL)"
	@rm -rf $(MAVEN_HOME) $(MAVEN_TARBALL) $(MAVEN_ENV)
	@$(MAKE) --no-print-directory clean-target 2>/dev/null || true

clean-target:
	@rm -rf target/

distclean: clean
	@echo "→ Removing entire tools/ directory"
	@rm -rf tools/

