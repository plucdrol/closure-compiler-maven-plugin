"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function merge(adapter, arr1, arr2) {
    var res = [];
    var len1 = arr1.length;
    var len2 = arr2.length;
    var i1 = 0;
    var i2 = 0;
    while ((i1 < len1) && (i2 < len2)) {
        var o1 = adapter.getData(arr1[i1]).order;
        var o2 = adapter.getData(arr2[i2]).order;
        if (o1 < o2) {
            i1 += 1;
            res.push(o1);
        }
        else {
            i2 += 1;
            res.push(o2);
        }
    }
    while (i1 < len1) {
        var o1 = adapter.getData(arr1[i1]);
        i1 += 1;
        res.push(o1.order);
    }
    while (i2 < len2) {
        var o2 = adapter.getData(arr2[i2]);
        i2 += 1;
        res.push(o2.order);
    }
    return res;
}
function sort(adapter, vertices) {
    return vertices.map(function (v) { return ({ key: adapter.getData(v).order, val: v }); }).sort(function (v1, v2) { return v1.key - v2.key; }).map(function (v) { return v.val; });
}
var PearceKellyDetector = (function () {
    function PearceKellyDetector() {
        this.id = 0;
        this.stack = [];
        this.deltaXyB = [];
        this.deltaXyF = [];
        this.freeStack = [];
    }
    PearceKellyDetector.prototype.map = function () {
        var clone = new PearceKellyDetector();
        clone.id = this.id;
        for (var _i = 0, _a = this.freeStack; _i < _a.length; _i++) {
            var item = _a[_i];
            clone.freeStack.push(item);
        }
        return clone;
    };
    PearceKellyDetector.prototype.isReachable = function (adapter, source, target) {
        if (source === target) {
            return true;
        }
        var targetOrder = adapter.getData(target).order;
        if (adapter.getData(source).order > targetOrder) {
            return false;
        }
        var reachable = !this.dfs_f(source, adapter, targetOrder);
        this.cleanAfterCycle(adapter);
        return reachable;
    };
    PearceKellyDetector.prototype.createVertexData = function (g) {
        var id = this.freeStack.pop();
        return {
            order: id !== undefined ? id : this.id++,
            visited: false,
        };
    };
    PearceKellyDetector.prototype.onVertexDeletion = function (g, vertex) {
        var data = g.getData(vertex);
        this.freeStack.push(data.order);
    };
    PearceKellyDetector.prototype.canAddEdge = function (g, from, to) {
        if (from === to) {
            return false;
        }
        return this.checkCycle(g, from, to);
    };
    PearceKellyDetector.prototype.supportsOrder = function () {
        return true;
    };
    PearceKellyDetector.prototype.getOrder = function (g, vertex) {
        return g.getData(vertex).order;
    };
    PearceKellyDetector.prototype.checkCycle = function (adapter, x, y) {
        var lb = adapter.getData(y).order;
        var ub = adapter.getData(x).order;
        this.deltaXyB = [];
        this.deltaXyF = [];
        if (lb < ub) {
            if (!this.dfs_f(y, adapter, ub)) {
                this.cleanAfterCycle(adapter);
                return false;
            }
            this.dfs_b(x, adapter, lb);
            this.reorder(adapter);
        }
        return true;
    };
    PearceKellyDetector.prototype.cleanAfterCycle = function (adapter) {
        this.stack = [];
        for (var n = this.deltaXyF.pop(); n !== undefined; n = this.deltaXyF.pop()) {
            adapter.getData(n).visited = false;
        }
    };
    PearceKellyDetector.prototype.dfs_f = function (first, adapter, ub) {
        this.stack.push(first);
        while (this.stack.length > 0) {
            var n = this.stack.pop();
            var nData = adapter.getData(n);
            if (nData.visited) {
                continue;
            }
            nData.visited = true;
            this.deltaXyF.push(n);
            for (var it_1 = adapter.getSuccessorsOf(n), res = it_1.next(); !res.done; res = it_1.next()) {
                var wData = adapter.getData(res.value);
                if (wData.order === ub) {
                    return false;
                }
                if (!wData.visited && wData.order < ub) {
                    this.stack.push(res.value);
                }
            }
        }
        return true;
    };
    PearceKellyDetector.prototype.dfs_b = function (first, adapter, lb) {
        this.stack.push(first);
        while (this.stack.length > 0) {
            var n = this.stack.pop();
            var nData = adapter.getData(n);
            if (nData.visited) {
                continue;
            }
            nData.visited = true;
            this.deltaXyB.push(n);
            for (var it_2 = adapter.getPredecessorsOf(n), res = it_2.next(); !res.done; res = it_2.next()) {
                var wData = adapter.getData(res.value);
                if (!wData.visited && lb < wData.order) {
                    this.stack.push(res.value);
                }
            }
        }
    };
    PearceKellyDetector.prototype.reorder = function (adapter) {
        this.deltaXyB = sort(adapter, this.deltaXyB);
        this.deltaXyF = sort(adapter, this.deltaXyF);
        var L = this.deltaXyB.concat(this.deltaXyF);
        for (var _i = 0, L_1 = L; _i < L_1.length; _i++) {
            var w = L_1[_i];
            adapter.getData(w).visited = false;
        }
        var R = merge(adapter, this.deltaXyB, this.deltaXyF);
        for (var i = 0, j = L.length; i < j; ++i) {
            adapter.getData(L[i]).order = R[i];
        }
    };
    return PearceKellyDetector;
}());

window.foobar = 42;
window.PearceKellyDetector = PearceKellyDetector; // end-of-file