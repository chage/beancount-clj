env:
	@echo "Please run . venv/bin/activate first."
	@echo "====================================="
	@echo "python -m venv venv"
	@echo ". venv/bin/activate"
	@echo "pip install beancount"

dev: env
	clojure -M:common:dev:nrepl
