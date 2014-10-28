# encoding: utf-8
# Можно делать реальные запросы и обрабатывать реальные данные
# http://docs.scipy.org/doc/scipy-0.14.0/reference/tutorial/io.html
#
# http://scikit-learn.org/stable/index.html

# 3rd party
import requests
import matplotlib.pyplot as plt
import numpy as np

# sys
import json


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


def main():
    # Http part
    server = 'http://1-dot-arched-glow-381.appspot.com'
    port = 80  # 80
    ajax = AppAjax(server, port)
    user_info = ajax.get_user_summary_sync()

    # get distribution
    research_ajax = ResearchAjax(server, port)

    work_path = user_info[1]  # FIXME: hard code
    print work_path.genNames[0], work_path.pageName
    arg0 = PathValue(work_path.pageName, work_path.genNames[0], 0)

    # Read
    distribution = research_ajax.get_distribution_sync(arg0)

    # to NumPy arrays
    disabled = []
    x_disabled = []
    all_points = []
    x_all_points = []
    active = []
    x_active = np.array([], dtype=np.uint32)
    for i, elem in enumerate(distribution):
        all_points.append(elem.frequency)
        x_all_points.append(i)

        if not elem.enabled:
            disabled.append(elem.frequency)
            x_disabled.append(i)
        else:
            active.append(elem.frequency)
            x_active = np.append(x_active, i)  # FIXME: bad!

    # Processing
    plt.plot(x_all_points, all_points, '-', x_disabled, disabled, 'v')
    plt.plot(-1 * x_active, active)
    plt.grid(True)
    plt.show()

    # Clustering - Lloyd
    pass


if __name__ == '__main__':
    main()