CHIBI_IMAGES = gfx/images/icons.png gfx/images/smallicons.gif gfx/images/textures32.png
CHIBI_BIN = bin/chibipaint/*

all: splash.jar chibi.jar app.jar

test.jar: bin/test/*
	jar -cfm test.in.jar jar-manifest -C bin/ test/
	mv test.in.jar test.jar

splash.jar: bootstrap.jar bin/splash/*
	jar -cfm splash.in.jar jar-manifest -C bin/ splash/
	jar -uf splash.in.jar -C bin/ bootstrap/
	mv splash.in.jar splash.jar
	
chibi.jar: bootstrap.jar ${CHIBI_IMAGES} ${CHIBI_BIN}
	jar -cfm chibi.in.jar jar-manifest -C bin/ chibipaint/
	jar -uf chibi.in.jar -C bin/ images/
	mv chibi.in.jar chibi.jar

app.jar: ${CHIBI_IMAGES} ${CHIBI_BIN}
	jar -cfme app.jar jar-manifest chibiapp.ChibiApp -C bin/ chibipaint/ 
	jar -uf app.jar -C bin/ chibiapp/
	jar -uf app.jar -C bin/ images/
	jar -uf app.jar -C bin/ splash/
	jar -uf app.jar -C bin/ bootstrap/
	jar -uf app.jar -C bin/ javax/

bootstrap.jar: bin/bootstrap/*
	jar -cfm bootstrap.jar jar-manifest -C bin/ bootstrap/

clean:
	rm -f chibi.jar    test.jar    app.jar         splash.jar
	rm -f chibi.in.jar test.in.jar splash.in.jar
