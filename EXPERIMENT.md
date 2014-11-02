Experiment
==========

This is a guide for setting up Syncope for an experiment regarding Java to
(relational) database interaction.

Prerequisites
-------------

1. Cloned repository (the target directory is referenced as ``SYNCOPE_DIR``)
```> git clone https://github.com/hschink/syncope.git```
2. Eclipse (tested with Luna release 4.4.0)
3. Eclipse Plug-In m2e

Installation
------------

1. In Eclipse go to _File > Import_ and select _Maven > Existing Maven Projects_
2. Select ``SYNCOPE_DIR`` as _Root Directory_ and select **only** ``/pom.xml``, ``build-tools/pom.xml``, ``common/pom.xml``, ``client/pom.xml``, and ``core/pom.xml``
3. Check installation by running the tests: On the project ``syncope-core`` go to _Run As > Maven test_
