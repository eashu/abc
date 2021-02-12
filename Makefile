LEADSERVICE_DJANGO_SETTINGS_MODULE ?= leadservice.settings.ci

ARTIFACTORY ?= artifactory-proxy.apps.fni
PYPI ?= http://${ARTIFACTORY}/artifactory/api/pypi/dtfni-pypi/simple/
PIP_INDEX = -i ${PYPI}
PIP_FLAGS ?= --trusted-host ${ARTIFACTORY}

comma := ,
empty :=
space := $(empty) $(empty)
CHECK_PACKAGES = lead leadservice utils
PYTHON_PACKAGES := $(subst $(space),$(comma),$(CHECK_PACKAGES))

DEPLOY_ENVIRONMENT ?= ci

TEST_ENV = DEPLOY_ENVIRONMENT=$(DEPLOY_ENVIRONMENT) DJANGO_SETTINGS_MODULE=$(LEADSERVICE_DJANGO_SETTINGS_MODULE)
TEST = nosetests
TEST_FLAGS = -sv --cover-package=$(PYTHON_PACKAGES) --with-xcoverage --with-xunit

logs:
	-sudo mkdir -p /var/log/dtplatform
	-sudo mkdir -p /var/log/dtweb
	-sudo mkdir -p /var/log/drs
	sudo chmod -R 777 /var/log/dtplatform
	sudo chmod -R 777 /var/log/dtweb
	sudo chmod -R 777 /var/log/drs

install:
	$(info *** ARTIFACTORY: $(ARTIFACTORY))
	$(info *** PYPI: $(PYPI))

	pip3 install versiontools $(PIP_INDEX) $(PIP_FLAGS)
	pip3 install -r requirements.txt $(PIP_INDEX) $(PIP_FLAGS)
	pip3 freeze

setup:
	dtconfig_setup
	dtconfig-data-local $(DEPLOY_ENVIRONMENT)

check:
	-eval time flake8 .

test_unit:
	$(TEST_ENV) $(TEST) $(TEST_FLAGS) tests/unit

test_integration:
	echo "As of Oct-15-2019, we do not have any integration tests."
# 	$(TEST_ENV) $(TEST) $(TEST_FLAGS) tests/integration

test:
	$(TEST_ENV) $(TEST) $(TEST_FLAGS)

build: logs install setup test check
ci-build: build
