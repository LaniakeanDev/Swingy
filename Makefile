# ----------------------------------------------------------------------
# Swingy — self-sufficient build
#
# Fresh clone:
#   make                # download JDK + Maven, build the jar
#   make mvn ARGS="clean package"    # run Maven directly
#   make run            # launch console mode
#   make gui            # launch GUI mode (requires a display)
#   make env            # print how to activate mvn/java in your shell
#   make clean          # remove tools/ and target/
#   make distclean      # also remove the JDK
# ----------------------------------------------------------------------

# ---- Tool versions ---------------------------------------------------
MAVEN_VERSION := 3.9.6
MAVEN_NAME    := apache-maven-$(MAVEN_VERSION)
MAVEN_URL     := https://archive.apache.org/dist/maven/maven-3/$(MAVEN_VERSION)/binaries/$(MAVEN_NAME)-bin.tar.gz
MAVEN_TARBALL := tools/$(MAVEN_NAME)-bin.tar.gz

# Temurin JDK 17 — full JDK, includes libawt_xawt.so (headful AWT).
# Override on the command line if you want a different version or arch:
#   make JDK_VERSION=17.0.2+8 JDK_ARCH=linux
JDK_VERSION   := 17.0.2+8
JDK_ARCH      := linux
JDK_NAME      := OpenJDK17U-jdk_x64_$(JDK_ARCH)_hotspot_$(subst +,_,$(JDK_VERSION))
JDK_URL       := https://github.com/adoptium/temurin17-binaries/releases/download/jdk-$(subst +,%2B,$(JDK_VERSION))/$(JDK_NAME).tar.gz
JDK_TARBALL   := tools/$(JDK_NAME).tar.gz

# ---- Directories -----------------------------------------------------
TOOLS_DIR     := tools
MAVEN_HOME    := $(TOOLS_DIR)/$(MAVEN_NAME)
MAVEN_BIN     := $(MAVEN_HOME)/bin
MVN           := $(MAVEN_BIN)/mvn
JDK_HOME      := $(TOOLS_DIR)/jdk
JAVA          := $(JDK_HOME)/bin/java
JAVAC         := $(JDK_HOME)/bin/javac
ENV_SCRIPT    := $(TOOLS_DIR)/env.sh

JAR           := target/swingy.jar

.PHONY: all install build run gui env mvn clean clean-target distclean

# ---- Default target --------------------------------------------------
all: build

# ---- Full setup + build ---------------------------------------------
install: $(MVN) $(JDK_HOME) $(ENV_SCRIPT)
build: $(JAR)

$(JAR): $(MVN) $(JDK_HOME) pom.xml
	@JAVA_HOME=$(abspath $(JDK_HOME)) $(MVN) -q clean package
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

# ---- Download JDK ----------------------------------------------------
$(JDK_TARBALL): | $(TOOLS_DIR)
	@echo "→ Downloading Temurin JDK $(JDK_VERSION) ($(JDK_ARCH))..."
	@curl -fL $(JDK_URL) -o $(JDK_TARBALL).tmp
	@if ! file -b $(JDK_TARBALL).tmp | grep -q 'gzip compressed'; then \
		echo "ERROR: JDK download is not a gzip tarball (mirror returned an error page)." >&2; \
		echo "       Check JDK_VERSION/JDK_ARCH, or download manually from:" >&2; \
		echo "       https://adoptium.net/temurin/releases/" >&2; \
		rm -f $(JDK_TARBALL).tmp; \
		exit 1; \
	fi
	@mv $(JDK_TARBALL).tmp $(JDK_TARBALL)
	@echo "✔ Downloaded $(JDK_TARBALL)"

# ---- Extract JDK -----------------------------------------------------
$(JDK_HOME): $(JDK_TARBALL)
	@echo "→ Extracting JDK..."
	@mkdir -p $(JDK_HOME)
	@tar -xzf $(JDK_TARBALL) -C $(JDK_HOME) --strip-components=1
	@echo "✔ JDK installed at $(JDK_HOME)"

# ---- Verify headful AWT library is present ---------------------------
# This is the whole point of bundling the JDK. If libawt_xawt.so is missing,
# Swing will silently force headless mode no matter what flags you pass.
check-jdk: $(JDK_HOME)
	@if [ ! -f "$(JDK_HOME)/lib/libawt_xawt.so" ]; then \
		echo "ERROR: $(JDK_HOME)/lib/libawt_xawt.so is missing." >&2; \
		echo "       This JDK is headless-only; Swing will not work." >&2; \
		exit 1; \
	fi
	@echo "✔ Headful AWT library present"

# ---- Write activation script ----------------------------------------
$(ENV_SCRIPT): $(MVN) $(JDK_HOME)
	@printf 'export JAVA_HOME="%s"\n' "$(abspath $(JDK_HOME))" > $(ENV_SCRIPT)
	@printf 'export PATH="%s:%s:$$PATH"\n' "$(abspath $(JDK_HOME))/bin" "$(abspath $(MAVEN_BIN))" >> $(ENV_SCRIPT)
	@echo "✔ Wrote $(ENV_SCRIPT)"

# ---- Convenience targets --------------------------------------------
mvn: $(MVN) $(JDK_HOME)
	@JAVA_HOME=$(abspath $(JDK_HOME)) $(MVN) $(ARGS)

run: $(JAR) check-jdk
	@$(JAVA) -jar $(JAR) console

gui: $(JAR) check-jdk
	@$(JAVA) -jar $(JAR) gui

env: $(ENV_SCRIPT)
	@echo "To use mvn and java from this project in your current shell, run:"
	@echo "    source $(ENV_SCRIPT)"

# ---- Cleanup ---------------------------------------------------------
clean:
	@echo "→ Removing Maven and build output (keeping JDK)"
	@rm -rf $(MAVEN_HOME) $(MAVEN_TARBALL) $(ENV_SCRIPT)
	@$(MAKE) --no-print-directory clean-target 2>/dev/null || true

clean-target:
	@rm -rf target/

distclean: clean
	@echo "→ Removing entire tools/ directory (including JDK)"
	@rm -rf tools/