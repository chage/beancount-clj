# beancount-clj

This project initially aimed to port [Beancount](https://github.com/beancount/beancount) into a Clojure library, but due to the extensive functionality, it shifted to using [libpython-clj](https://github.com/clj-python/libpython-clj) to load Beancount instead.

## Getting Started

### Add beancount-clj as a dependency

```
{:deps {clj-python/libpython-clj {:mvn/version "2.024"}
        com.github.chage/beancount-clj {:git/sha "..."}
```

### Python

Currently you need to run under Python `venv`. And remember to install `beancount`.

```
$ python -m venv venv
$ . venv/bin/activate
(venv) $ pip install beancount
```

