from verta import Client

client = Client(host="trial.dev.verta.ai")
project = client.set_project("my project")
exp = client.set_experiment("my exp")

try:
    for i in range(11):
        client.set_experiment_run("some-run-{}".format(i))
except Exception as err:
    print(err)
# finally:
#     project.delete()