.PHONY: all data sass serve

all: data sass cljs

#
# Development
# ===========
#

DATA =  resources/data/places.json

resources/%.json: %.yaml
	mkdir -p $(@D)
	python -c 'import sys, yaml, json; \
		   json.dump(yaml.load(sys.stdin), sys.stdout, indent=4)' \
		< $< > $@

data: $(DATA)

sass:
	compass compile

cljs:
	lein cljsbuild once

figwheel:
	rlwrap lein figwheel

serve:
	coffee server.coffee

# pip install watchdog
watch-data:
	watchmedo shell-command \
		--recursive \
		--command="$(MAKE) data" \
		data/

watch-sass:
	compass watch

watch-cljs:
	lein cljsbuild auto debug

dev:
	trap "trap - TERM && kill 0" EXIT TERM INT; \
	$(MAKE) watch-data & \
	$(MAKE) watch-sass & \
	$(MAKE) serve & \
	$(MAKE) figwheel

#
# Deployment
# ==========
#

upload:
	ncftpput -z -m -R -f host.ncftpput /mittagessen resources/* resources/views/index.html
