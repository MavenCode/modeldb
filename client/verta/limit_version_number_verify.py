from verta import Client

client = Client(host="trial.dev.verta.ai")
model = client.set_registered_model("my model")

try:
    for i in range(11):
        model.create_version("some-version-{}".format(i))
except Exception as err:
    print(err)
finally:
    model.delete()