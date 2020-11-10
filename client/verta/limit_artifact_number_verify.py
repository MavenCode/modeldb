from verta import Client
import os
import six
import pickle


def generate_random_data():
    while True:
        data = os.urandom(2 ** 16) # around 65 kbs
        bytestream = six.BytesIO(data)
        try:
            pickle.load(bytestream)
        except:
            return data

client = Client(host="trial.dev.verta.ai")
project = client.set_project("my project 2")
exp = client.set_experiment("my exp")
run = client.set_experiment_run("my run")
filenames = []

try:
    for i in range(11):
        filename = "file_{}".format(i)
        filenames.append(filename)
        FILE_CONTENTS = generate_random_data()
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)

        run.log_artifact("file_{}".format(i), filename)
except Exception as err:
    print(err)
finally:
    project.delete()
    for filename in filenames:
        os.remove(filename)
