import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

data = pd.read_csv('kpi.jtl', header=0)

errors = data[ data['success'] == False ]
oks = data[ data['success'] ]

print( errors )

toPlot = oks[[ 'elapsed', 'label' ]].set_index('label')

toPlot.boxplot(by='label', rot=45, fontsize=12, figsize=(8,10))
plt.savefig('chart.png')


def percentile(n):
  def percentile_(x):
    return np.percentile(x, n)
  percentile_.__name__ = 'percentile_%s' % n
  return percentile_

#oks[[ 'elapsed', 'label' ]].agg([ np.mean, np.std, np.median, np.min, np.max], by='label').to_csv('summary.txt')

data_summary = toPlot.groupby(level='label').agg([ np.mean, np.std, np.median, percentile(90), percentile(95), percentile(99)])
print( data_summary )
data_summary.to_json('summary.json')


print( toPlot.groupby(level='label').size() )
