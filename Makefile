
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



	








# # ----------------------------------------------------------------------
# # Maven local installer — installs into ./tools/maven/ inside the project
# #
# # Usage:
# #   make install      # full install (runs everything below in order)
# #   make download     # just fetch the tarball
# #   make extract      # just unpack it
# #   make mvn          # run the project-local mvn on the project
# #   make verify       # check the project-local mvn works
# #   make clean        # remove tools/maven/ and the cached tarball
# # ----------------------------------------------------------------------

# # ---- Configuration ---------------------------------------------------
# MAVEN_VERSION := 3.9.6
# MAVEN_NAME    := apache-maven-$(MAVEN_VERSION)
# MAVEN_URL := https://archive.apache.org/dist/maven/maven-3/$(MAVEN_VERSION)/binaries/$(MAVEN_NAME)-bin.tar.gz
# MAVEN_TARBALL := tools/$(MAVEN_NAME)-bin.tar.gz

# # Everything Maven lives under ./tools/, relative to the Makefile.
# INSTALL_DIR   := tools
# MAVEN_HOME    := $(INSTALL_DIR)/$(MAVEN_NAME)
# MAVEN_BIN     := $(MAVEN_HOME)/bin
# MVN           := $(MAVEN_BIN)/mvn

# # ---- Phony targets ---------------------------------------------------
# .PHONY: install download extract verify mvn clean

# # ---- 1. Top-level target ---------------------------------------------
# install: verify
# 	@echo ""
# 	@echo "✔ Maven $(MAVEN_VERSION) installed at $(MAVEN_HOME)"
# 	@echo ""
# 	@echo "To use mvn in this terminal, run:"
# 	@echo "    export PATH=\"$(abspath $(MAVEN_BIN)):\$$PATH\""

# # ---- 2. Download the tarball into ./tools/ --------------------------
# tools:
# 	@mkdir -p tools

# $(MAVEN_TARBALL): | tools
# 	@echo "→ Downloading Maven $(MAVEN_VERSION) into $(MAVEN_TARBALL)..."
# 	curl -Lo $(MAVEN_TARBALL) $(MAVEN_URL)

# download: $(MAVEN_TARBALL)

# # ---- 3. Extract into ./tools/ ---------------------------------------
# $(MVN): $(MAVEN_TARBALL)
# 	@echo "→ Extracting into $(INSTALL_DIR)..."
# 	tar -xzf $(MAVEN_TARBALL) -C $(INSTALL_DIR)

# extract: $(MVN)

# # ---- 4. Verify -------------------------------------------------------
# verify: extract
# 	@echo "→ Verifying installation..."
# 	@$(MVN) -v

# # ---- 5. Convenience: run mvn from the project -----------------------
# # e.g.  make mvn ARGS="clean package"
# mvn: extract
# 	@$(MVN) $(ARGS)

# # ---- 6. Cleanup ------------------------------------------------------
# clean:
# 	@echo "→ Removing $(MAVEN_HOME) and $(MAVEN_TARBALL)"
# 	rm -rf $(MAVEN_HOME)
# 	rm -f  $(MAVEN_TARBALL)

# run:
# 	@echo "→ Running the project..."
# 	java -jar target/swingy.jar console










# # Create a directory for local software (if it doesn't exist)
# mkdir -p ~/.local/opt

# # Download the latest Maven (check the Apache website for the newest version)
# # You can use curl or wget. Here we download to /tmp first.
# curl -LO https://dlcdn.apache.org/maven/maven-3/3.9.14/binaries/apache-maven-3.9.14-bin.tar.gz

# # Extract it into your chosen directory
# tar -xvzf apache-maven-3.9.14-bin.tar.gz -C ~/.local/opt/

# # Add Maven's bin directory to your PATH
# echo 'export PATH="$HOME/.local/opt/apache-maven-3.9.14/bin:$PATH"' >> ~/.bashrc

# # Apply the Changes
# source ~/.bashrc

# # Verify Installation
# mvn -v
