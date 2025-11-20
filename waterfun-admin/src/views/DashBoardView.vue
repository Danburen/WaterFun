<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import {BarChart, PieChart} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent, GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

// 注册必要的ECharts组件
use([
  CanvasRenderer,
  PieChart,
  BarChart,
  GridComponent,
  TitleComponent,
  TooltipComponent,
  LegendComponent
])

// 图表配置 - 简化初始配置
const option = ref({
  title: {
    text: '销售数据统计',
    subtext: '动态数据来自MySQL数据库',
    left: 'center'
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    },
  },
  legend: {
    orient: 'vertical',
    data: ['销售额'],
    top: 'bottom'
  },
  xAxis:{
    type: 'category',
    data: [],
    axisLabel: {
      rotate: 30 // 如果标签文字太长可以旋转
    }
  },
  yAxis: {
    type: 'value',
    name: '销售额'
  },
  series: [
    {
      type: 'bar',
      data: [], // 初始为空数组
      itemStyle: {
        color: function(params) {
          // 自定义颜色
          var colorList = ['#c23531','#2f4554','#61a0a8','#d48265','#91c7ae'];
          return colorList[params.dataIndex % colorList.length];
        }
      }
    }
  ]
})

// 加载数据
const loading = ref(true)
const error = ref(null)

const fetchSalesData = async () => {
  try {
    const response = await fetch('http://localhost:8080/api/dashboard/sales-data')
    if (!response.ok) throw new Error('获取数据失败')

    const data = await response.json()
    console.log('API返回数据:', data) // 调试用

    // 验证数据格式
    if (!Array.isArray(data)) {
      throw new Error('数据格式错误: 期望数组')
    }

    // 转换数据格式，确保每个项都有name和value
    const chartData = data.map(item => {
      if (!item.productName || item.salesAmount === undefined) {
        console.warn('无效数据项:', item)
      }
      return {
        name: item.productName || '未知产品',
        value: item.salesAmount || 0
      }
    })

    // 完全替换option对象确保响应式更新
    option.value = {
      ...option.value,
      series: [{
        ...option.value.series[0],
        data: chartData
      }],
      xAxis: {
        ...option.value.xAxis[0],
        data: chartData.map(item => item.name)
      }
    }

  } catch (err) {
    console.error('获取数据出错:', err)
    error.value = err.message || '发生未知错误'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchSalesData()
})
</script>

<template>
  <div class="dashboard-container">
    <h1>销售数据仪表盘</h1>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="option.series[0].data.length > 0" class="chart-container">
      <v-chart
          class="chart"
          :option="option"
          :autoresize="true"
      />
    </div>
    <div v-else class="loading">无可用数据</div>
  </div>
</template>

<style scoped>
.dashboard-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.chart-container {
  width: 100%;
  height: 500px;
}

.chart {
  width: 100%;
  height: 100%;
}

.loading, .error {
  text-align: center;
  padding: 50px;
  font-size: 18px;
}

.error {
  color: red;
}
</style>