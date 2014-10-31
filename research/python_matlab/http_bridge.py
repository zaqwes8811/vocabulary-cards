# encoding: utf-8
# Можно делать реальные запросы и обрабатывать реальные данные
# http://docs.scipy.org/doc/scipy-0.14.0/reference/tutorial/io.html
#
# http://scikit-learn.org/stable/index.html

# 3rd party
import requests
import matplotlib.pyplot as plt
import numpy as np
import numpy.random
import sklearn.cluster

# sys
import json
import time
import random


class PathValue(object):
    def __init__(self, page, gen, pos):
        self.pageName = page
        self.genName = gen
        self.pointPos = pos

    def assign_deserialized(self, obj):
        self.pageName = obj['pageName']
        self.genName = obj['genName']
        self.pointPos = obj['pointPos']


class UserInfoValue(object):
    def __init__(self, obj):
        self.pageName = None
        self.genNames = None
        self.pointPos = None
        self.assign_deserialized(obj)

    def assign_deserialized(self, obj):
        self.pageName = obj['pageName']
        self.genNames = obj['genNames']
        # self.pointPos = obj['pointPos']  # no exist. was bug coupled with it


class DistributionElem(object):
    def __init__(self, elem):
        self.frequency = elem['frequency']
        self.enabled = elem['enabled']


class AppAjax(object):
    def __init__(self, url, port):
        self.server = url + ":" + str(port)

    def _build_url(self, uri):
        return self.server + uri

    def get_user_summary_sync(self):
        uri = '/user_summary'
        r = requests.get(self._build_url(uri))
        r.raise_for_status()

        tmp = r.json()
        result = []
        for val in tmp:
            result.append(UserInfoValue(val))
        return result


class ResearchAjax(object):
    def __init__(self, url, port):
        self.server = url + ":" + str(port)

    def _build_url(self, uri):
        return self.server + uri

    def get_distribution_sync(self, arg0):
        uri = '/research/get_distribution'
        payload = {'arg0': json.dumps(arg0, default=lambda o: o.__dict__, sort_keys=True)}
        r = requests.get(self._build_url(uri), params=payload)
        r.raise_for_status()

        tmp = r.json()
        result = []
        for val in tmp:
            result.append(DistributionElem(val))
        return result

    def get_pure_distribution(self, arg0):
        d = self.get_distribution_sync(arg0)
        frequencies = []
        for i, elem in enumerate(d):
            freq = elem.frequency
            frequencies.append(freq)
        return frequencies

    def create_or_replace_page(self):
        # Просто через post
        test_file = '../test_data/etalon.srt'
        with open(test_file, "r") as file_handle:
            data = file_handle.read()

        url = '/research/accept_text'

        payload = {'name': self.get_research_page_name(), 'text': data}
        r = requests.post(self._build_url(url), data=json.dumps(payload))
        r.raise_for_status()

    @staticmethod
    def get_research_page_name():
        return 'research_page'


def plot_distribution(d):
    # to NumPy arrays
    disabled = []
    x_disabled = []
    all_points = []
    x_all_points = []
    active = []
    x_active = np.array([], dtype=np.uint32)
    for i, elem in enumerate(d):
        all_points.append(elem.frequency)
        x_all_points.append(i)

        if not elem.enabled:
            disabled.append(elem.frequency)
            x_disabled.append(i)
        else:
            active.append(elem.frequency)
            x_active = np.append(x_active, i)  # FIXME: bad!

    # Processing
    if False:
        plt.plot(x_all_points, all_points, '-', x_disabled, disabled, 'v')
        plt.plot(-1 * x_active, active)
        plt.grid(True)
        plt.show()


def unroll_distribution(d):
    X = []
    Y = []
    for i, elem in enumerate(d):
        for j in range(elem):
            X.append([i])
            Y.append(random.gauss(0, 0.1))
    return np.array(X), np.array(Y)


def main():
    # Http part
    server = 'http://localhost'
    port = 8080
    research_ajax = ResearchAjax(server, port)
    # research_ajax.create_or_replace_page()

    research_ajax = ResearchAjax(server, port)
    arg0 = PathValue(research_ajax.get_research_page_name(), "Default", 0)

    # Read
    d = research_ajax.get_pure_distribution(arg0)
    #plot_distribution(d)

    # Clustering - kMean
    # Expand data for training
    # FIXME: а может расщеплять не нужно, а так скромить кластеризатору?
    X, Y = unroll_distribution(d)

    # http://www.slideshare.net/SarahGuido/estimator-clustering-with-scikitlearn
    # распределят почти равномерно - сложное распределение - похоже кластеризатор считает его почти равномерным
    # да, пик есть, но это мало меняет, он узкий и не может сильно сузить кластер
    n_clusters = 3
    estimator = sklearn.cluster.KMeans(k=n_clusters, max_iter=300)
    x_active = np.array(X, dtype=np.uint64)  # positions

    np.random.shuffle(x_active)  # для кластеризации похоже не важно

    # FIXME: имена кластеров перемешаны!
    assignments = estimator.fit_predict(x_active)

    for j in range(n_clusters):
        z = np.where(assignments == j)
        x = x_active[z].ravel()
        y = (np.ones(x.shape) + np.random.laplace(size=x.shape)).ravel()
        plt.plot(x, y, 'o')

    # Lloyd - это алгоритмы решения, а не алгоритм обучения, похоже

    # Nearest Neighbors version
    plt.grid(True)
    plt.show()


if __name__ == '__main__':
    main()
