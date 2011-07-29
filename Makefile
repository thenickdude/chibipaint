CHIBI_IMAGES = gfx/images/icons.png gfx/images/smallicons.gif gfx/images/textures32.png
CHIBI_BIN = bin/chibipaint/ bin/chibitest/

all: splash.jar chibi.jar

splash.jar: bin/splash/*
	jar -cf $@ -C bin/ splash/
	
chibi.jar: ${CHIBI_IMAGES} ${CHIBI_BIN}
	jar -cf $@ -C bin/ chibipaint/
	jar -uf $@ -C bin/ images/

clean:
	rm -f splash.jar
	rm -f chibi.jar

install:
	cp chibi.jar ../trunk/public_html/oekaki/chibi/
	cp splash.jar ../trunk/public_html/oekaki/chibi/