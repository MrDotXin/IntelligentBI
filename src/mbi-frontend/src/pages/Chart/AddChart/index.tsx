import { getChartByIdUsingGet, uploadChartRequestAsyncUsingPost, uploadChartRequestUsingPost } from '@/services/mbi-backend/chartController';
import { InboxOutlined } from '@ant-design/icons';
import { heILIntl } from '@ant-design/pro-components';
import {
  Button,
  Card,
  Form,
  Input,
  message,
  Select,
  Spin,
  Splitter,
  Typography,
  Upload,
} from 'antd';
import { createStyles } from 'antd-style';
import type { UploadFile } from 'antd/es/upload/interface';
import ReactECharts from 'echarts-for-react';
import React, { useState } from 'react';

const { Title, Text } = Typography;

const useStyles = createStyles(({ token }) => ({
  formCard: {
    height: '100%',
    borderRadius: '20px',
    background: 'white',
    boxShadow: '0 12px 32px rgba(0,0,0,0.08)',
    display: 'flex',
    flexDirection: 'column',
    overflow: 'auto',
  },
  resultCard: {
    height: '100%',
    borderRadius: '20px',
    background: 'white',
    boxShadow: '0 12px 32px rgba(0,0,0,0.08)',
    display: 'flex',
    flexDirection: 'column',
    overflow: 'auto',
  },
  analysisResult: {
    flex: '0 0 auto',
    backgroundColor: '#f8fafc',
    borderRadius: '12px',
    padding: '24px',
    margin: '16px 0',
    lineHeight: 1.25,
    fontSize: '16px',
    color: '#64748b',
    whiteSpace: 'pre-wrap',
    maxHeight: '300px', // 限制高度并启用滚动
    overflowY: 'auto',
  },
  uploadArea: {
    border: `2px dashed ${token.colorPrimary}`,
    borderRadius: '12px',
    background: 'rgba(99, 102, 241, 0.03)',
    transition: 'all 0.3s',
    '&:hover': {
      borderColor: '#6366f1',
      background: 'rgba(99, 102, 241, 0.05)',
    },
  },
  formTitle: {
    fontSize: '24px',
    fontWeight: 600,
    color: '#1e293b',
    marginBottom: '32px',
    position: 'relative',
    '&::after': {
      content: '""',
      position: 'absolute',
      bottom: '-12px',
      left: 0,
      width: '48px',
      height: '3px',
      background: 'linear-gradient(90deg, #6366f1 0%, #8b5cf6 100%)',
      borderRadius: '2px',
    },
  },
  submitButton: {
    height: '48px',
    fontSize: '16px',
    background: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
    borderRadius: '10px',
    '&:hover': {
      transform: 'scale(1.01)',
      boxShadow: '0 8px 16px rgba(99, 102, 241, 0.2)',
    },
  },
}));

const Login: React.FC = () => {
  const [chart, setChart] = useState<API.BIChartResponse>();
  const [option, setOption] = useState<any>();
  const { styles } = useStyles();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [drawerRatio, setDrawerRatio] = useState(85);
  const [chartRatio, setChartRatio] = useState(50);

  const onFinish = async (values: any) => {
    console.log(values);
    try {
      setLoading(true);

      // 确保有文件上传
      if (fileList.length === 0) {
        message.error('请上传数据文件');
        return;
      }

      const file = fileList[0].originFileObj;
      if (!file) {
        message.error('文件无效');
        return;
      }

      const response = await uploadChartRequestAsyncUsingPost(
        {
          chartName: values.chartName,
          chartType: values.chartType,
          goal: values.goal,
        },
        {},
        file,
      );

      // 设置定时器, 每3秒读取一次后台数据
      if (response.code === 0) {
        const timer = setInterval(async () => {
          const chartDataResponse = await getChartByIdUsingGet({
            id: response.data
          });
          if (chartDataResponse.data?.genState === 'complete') {
            displayDataFull();
            setLoading(false);
            setChart({
              chartData: chartDataResponse.data?.genChart,
              chartResult: chartDataResponse.data?.genResult
            });
            setOption(JSON.parse(chartDataResponse.data?.genChart || '{}'))
            clearInterval(timer); 
          } 
        }, 3000);
      }


    } catch (e: any) {
      message.error('分析失败，' + (e.message || '未知错误'));
    } 
  };

  const beforeUpload = (file: File) => {
    const isLt10M = file.size / 1024 / 1024 < 10;
    if (!isLt10M) {
      message.error('文件大小不能超过10MB');
      return false;
    }
    return true;
  };

  const handleChange = (info: { fileList: UploadFile[] }) => {
    setFileList(info.fileList);
  };

  const formItemLayout = {
    labelCol: { span: 4 },
    wrapperCol: { span: 18 },
  };

  const displayDataFull = () => {
    setDrawerRatio(10);
    setChartRatio(25);
  }

  return (
    <div>
      <div>
        <Splitter onResize={(sizes: number[]) => { setDrawerRatio(Math.round((sizes[0] / (sizes[0] + sizes[1])) * 100)); }}>
          <Splitter.Panel size={drawerRatio + '%'} collapsible>
            <Card className={styles.formCard} title="填入数据">
              <Form
                form={form}
                {...formItemLayout}
                onFinish={onFinish}
                initialValues={{ chartType: 'bar chart' }}
              >
                <Form.Item
                  label={<Text strong>图表标题</Text>}
                  name="chartName"
                  rules={[{ required: true, message: '请输入图表标题' }]}
                >
                  <Input
                    size="large"
                    placeholder="请输入图表标题"
                    allowClear
                    style={{ borderRadius: '8px' }}
                  />
                </Form.Item>

                <Form.Item
                  label={<Text strong>图表类型</Text>}
                  name="chartType"
                  rules={[{ required: true, message: '请选择图表类型' }]}
                >
                  <Select
                    size="large"
                    options={[
                      { value: 'bar chart', label: '柱状图' },
                      { value: 'line chart', label: '折线图' },
                      { value: 'pie chart', label: '饼图' },
                      { value: 'radar chart', label: '雷达图' },
                      { value: 'stacked chart', label: '堆叠图' },
                    ]}
                    style={{ borderRadius: '8px' }}
                  />
                </Form.Item>

                <Form.Item
                  label={<Text strong>分析目标</Text>}
                  name="goal"
                  rules={[{ required: true, message: '请输入分析目标' }]}
                >
                  <Input.TextArea
                    size="large"
                    placeholder="示例: 请分析用户增长趋势并预测未来3个月变化"
                    rows={4}
                    style={{ borderRadius: '8px' }}
                  />
                </Form.Item>

                <Form.Item
                  label={<Text strong>数据文件</Text>}
                  required
                  rules={[{ required: true, message: '请上传数据文件' }]}
                >
                  <Upload.Dragger
                    name="file"
                    fileList={fileList}
                    beforeUpload={beforeUpload}
                    onChange={handleChange}
                    maxCount={1}
                    className={styles.uploadArea}
                  >
                    <p className="ant-upload-drag-icon">
                      <InboxOutlined
                        style={{
                          fontSize: '40px',
                          color: '#6366f1',
                          background: 'rgba(99, 102, 241, 0.1)',
                          borderRadius: '50%',
                          padding: '12px',
                        }}
                      />
                    </p>
                    <p className="ant-upload-text" style={{ fontSize: '16px', margin: '8px 0' }}>
                      <Text strong>点击上传或拖拽文件至此</Text>
                    </p>
                    <p className="ant-upload-hint" style={{ color: '#64748b' }}>
                      支持 CSV/Excel 文件, 建议文件大小不超过10MB
                    </p>
                  </Upload.Dragger>
                </Form.Item>

                <Form.Item wrapperCol={{ offset: 4, span: 18 }}>
                  <Button
                    className={styles.submitButton}
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    block
                  >
                    {loading ? '🚀智能分析中...' : '立即生成分析报告'}
                  </Button>
                </Form.Item>
              </Form>
            </Card>
          </Splitter.Panel>
          <Splitter.Panel>
            <Card className={styles.resultCard} title="智能分析结果">
              <Spin spinning={loading} tip="智能分析中请稍等..." size="large">
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                  {/* 分析结论部分 */}
                  <Title
                    level={5}
                    style={{
                      color: '#1e293b',
                      marginBottom: '24px',
                      flex: '0 0 auto',
                    }}
                  >
                    分析结论
                  </Title>
                  <Splitter 
                      onResize={(sizes: number[]) => { setChartRatio(Math.round((sizes[0] / (sizes[0] + sizes[1])) * 100)); }} 
                      layout="vertical" style={{ height: '100vh' }}>
                    <Splitter.Panel size={chartRatio + "%"}>
                      <div className={styles.analysisResult}>
                        {chart?.chartResult || (
                          <Text type="secondary">提交数据后自动生成分析结论</Text>
                        )}
                      </div>
                    </Splitter.Panel>

                    {/* 图表展示部分 */}
                    <Splitter.Panel>
                      <div>
                        <Title
                          level={5}
                          style={{
                            color: '#1e293b',
                            marginBottom: '24px',
                            flex: '0 0 auto',
                          }}
                        >
                          数据可视化
                        </Title>
                        <div>
                          {option ? (
                            <ReactECharts option={option} />
                          ) : (
                            <div
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                color: '#94a3b8',
                                fontSize: '16px',
                              }}
                            >
                              <Text type="secondary">可视化图表将在此处展示</Text>
                            </div>
                          )}
                        </div>
                      </div>
                    </Splitter.Panel>
                  </Splitter>
                </div>
              </Spin>
            </Card>
          </Splitter.Panel>
        </Splitter>
      </div>
    </div>
  );
};

export default Login;
