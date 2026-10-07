
Installation

```bash
make install && source ~/.bashrc
```


mvn clean package

java -jar target/swingy.jar gui

find . -name "*.java" -exec sh -c 'printf "\n\n\n===== %s =====\n" "$1"; cat "$1"' _ {} \; > sources.txt


test: mvn test