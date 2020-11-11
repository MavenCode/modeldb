from verta import Client
from verta._internal_utils._utils import generate_default_name
from verta.endpoint.autoscaling import Autoscaling
from verta.endpoint.resources import Resources
from verta.endpoint.update import CanaryUpdateStrategy
from verta.endpoint.update.rules import MaximumServerErrorPercentageThresholdRule
from verta.endpoint.autoscaling.metrics import CpuUtilizationTarget
from sklearn.linear_model import LogisticRegression

client = Client(host="trial.dev.verta.ai")

endpoint = client.set_endpoint(generate_default_name())
proj = client.set_project()
run = client.set_experiment_run()

model = LogisticRegression()
run.log_model(model, custom_modules=[])
run.log_requirements(['scikit-learn'])

try:
    autoscaling = Autoscaling()
    autoscaling.add_metric(CpuUtilizationTarget(0.7))
    endpoint.update(run, autoscaling=autoscaling)
except Exception as err:
    print(err)

try:
    resources = Resources(memory="512Mi")
    endpoint.update(run, resources=resources)
except Exception as err:
    print(err)

try:
    strategy = CanaryUpdateStrategy(12, 0.1)
    strategy.add_rule(MaximumServerErrorPercentageThresholdRule(0.4))
    endpoint.update(run, strategy=strategy)
except Exception as err:
    print(err)

endpoint.delete()
proj.delete()