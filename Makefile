VER = $(shell lein pprint :version)

package:	
	lein clean
	lein uberjar
	rm -rf pkg/lib 2>/dev/null
	mkdir -p pkg/lib
	cp target/*-standalone.jar pkg/lib
	cp target/*-standalone.jar pkg/lib/monocle.jar
	cd pkg; tar cf - * | gzip >../monocle-$(VER).tar.gz
