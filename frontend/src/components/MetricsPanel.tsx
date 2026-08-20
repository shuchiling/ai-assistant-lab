import { useEffect, useRef } from 'react';
import * as echarts from 'echarts';

type MetricsPanelProps = {
  metrics: {
    totalMessages: number;
    assistantMessages: number;
    averageAnswerLength: number;
  };
};

export function MetricsPanel({ metrics }: MetricsPanelProps) {
  const chartRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!chartRef.current) return;
    const chart = echarts.init(chartRef.current);
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 36, right: 16, top: 32, bottom: 32 },
      xAxis: { type: 'category', data: ['Messages', 'Answers', 'Avg chars'] },
      yAxis: { type: 'value' },
      series: [
        {
          type: 'bar',
          data: [metrics.totalMessages, metrics.assistantMessages, metrics.averageAnswerLength],
          itemStyle: { color: '#2563eb' },
          barWidth: 34
        }
      ]
    });

    const resize = () => chart.resize();
    window.addEventListener('resize', resize);
    return () => {
      window.removeEventListener('resize', resize);
      chart.dispose();
    };
  }, [metrics]);

  return (
    <aside className="panel metricsPanel" aria-label="Chat metrics">
      <div className="panelHeader">
        <div>
          <h2>Metrics</h2>
          <p>Local conversation counters for the lab UI.</p>
        </div>
      </div>
      <div className="metricGrid">
        <Metric label="Messages" value={metrics.totalMessages} />
        <Metric label="Answers" value={metrics.assistantMessages} />
        <Metric label="Avg chars" value={metrics.averageAnswerLength} />
      </div>
      <div ref={chartRef} className="chart" />
    </aside>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="metricCard">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
