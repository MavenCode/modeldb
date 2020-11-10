from verta import Client
from verta._internal_utils._utils import generate_default_name
from verta.endpoint.autoscaling import Autoscaling
from verta.endpoint.autoscaling.metrics import CpuUtilizationTarget
from sklearn.linear_model import LogisticRegression

client = Client(host="trial.dev.verta.ai")

endpoint = client.set_endpoint(generate_default_name())
proj = client.set_project()
run = client.set_experiment_run()

try:
    model = LogisticRegression()
    run.log_model(model, custom_modules=[])
    run.log_requirements(['scikit-learn'])

    autoscaling = Autoscaling()
    autoscaling.add_metric(CpuUtilizationTarget(0.7))
    endpoint.update(model, autoscaling=autoscaling)
except Exception as err:
    print(err)
finally:
    endpoint.delete()
    proj.delete()