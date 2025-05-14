import { listMyChartByPageUsingPost } from '@/services/mbi-backend/chartController';
import { FieldTimeOutlined, LikeOutlined, MessageOutlined, StarOutlined } from '@ant-design/icons';
import {
  Avatar,
  Card,
  List,
  message,
  Popover,
  Space,
  TimePicker,
  Typography,
  Input,
  Spin,
  Descriptions,
} from 'antd';
import type { DescriptionsProps, GetProps } from 'antd';
import { createStyles } from 'antd-style';
import React, {Children, useEffect, useState} from 'react';
import EChartsReact from 'echarts-for-react';
import { useModel } from '@umijs/max';
import { Item, ItemGroup } from 'rc-menu';
const { Title, Text } = Typography;

type SearchProps = GetProps<typeof Input.Search>;

const { Search } = Input;

const useStyles = createStyles(({ token }) => ({
  ChartsItemContainer: {
    boxShadow: '0 0 16px rgba(0, 0, 0, 0.15);',
    margin: '10px',
    '&:hover': {
      cursor: 'pointer',
      transform: 'scale(1.002);',
      boxShadow: '0 0 32px rgba(0, 0, 0, 0.15);',
      transition: 'transform 0.1s ease-in-out',
    }
  }

}));

const Login: React.FC = () => {
  const { styles } = useStyles();

  const initSearchParams = {
    current: 1,
    pageSize: 6,

    // 排序字段, 默认按照生成的时间降序排序
    sortField: 'createTime',
    sortOrder: 'DESC'
  };

  const initialState = useModel("@@initialState");
  const [searchParams, setSearchParams] = useState<API.ChartQueryRequest>({...initSearchParams});
  const [chartList, setChartList] = useState<API.Chart[]>([]);
  const [total, setTotal] = useState<number>(0);

  const [onLoading, setOnLoading] = useState<boolean>(false);
  const loadData = async () => {
    console.log(initialState);
    setOnLoading(true);
    try {
      const response = await listMyChartByPageUsingPost(searchParams);
      if (response.code === 0) {
        setChartList(response.data?.records || []);
        setTotal(response.data?.total || 0);
      }
    } catch (error) {
      message.error(" 获取数据错误! " + error);

    }

    setOnLoading(false);
  }

  const IconText = ({ icon, text }: { icon: React.FC; text: string }) => (
    <Space>
      {React.createElement(icon)}
      {text}
    </Space>
  );

  const popOverContent = (text : string) => (
    <div style={{width: '40vw', height: '30vh', overflow: 'auto'}}>
      <Text>{text}</Text>
    </div>
  );

  const formatter = new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
  const toReadableDate = (dateStr : string) => (
    dateStr === '' ? '未知' : formatter.format(new Date(dateStr))
  );

  const onSearch = (value: string) => {
    setSearchParams({
      ...initSearchParams,
      chartName: value
    });
  }

  const getChartDescription = (item : API.Chart) => (
    <Descriptions
      column={2}
      title="信息描述"
      items={[
        {
          key: '1',
          label: '图表名称',
          children: item.chartName
        },
        {
          key: '1',
          label: '图表类型',
          children: item.chartType
        },
        {
          key: '1',
          label: '图表分析需求',
          children:
            <div style={{ overflow: 'auto', width: `30vw`, height: '4vw' }}>
              {item.goal}
            </div>
        },
      ]} />
  );


  useEffect(()=>{
      loadData();
    },
    [searchParams]
  );

  return (
    <div>
    <Search placeholder='输入表名' allowClear enterButton="搜索" size="large" onSearch={onSearch} />
    <List
    loading={onLoading}
    grid={{
      gutter: 16,
      xs: 1,
      sm: 1,
      md: 1,
      lg: 2,
      xl: 2,
      xxl: 3,
    }}
    pagination={{
      onChange: (page, pageSize) => {
        setSearchParams({
          ...initSearchParams,
          current: page,
          pageSize: pageSize
        });
      },
      current: searchParams.current,
      pageSize: searchParams.pageSize,
      total: total
    }}
    dataSource={chartList}
    footer={
      <div>
        <b>你的</b> 图表, 共计 <b>{ total }</b> 条
      </div>
    }
    renderItem={(item) => (
      <Popover
        placement="right"
        title={item.chartName}
        content={popOverContent(item.genResult || '')}
        trigger={'click'}
      >
      <Card className={ styles.ChartsItemContainer }>
        <List.Item
          key={item.id}
        >
          <List.Item.Meta
            avatar={
            <Avatar src={
              initialState.initialState?.currentUser?.userAvatar || 'https://jfcd2024.oss-cn-shanghai.aliyuncs.com/avatar.jpg'}
            />
            }
            title={"你"}
            description={"图表类型: " + item.chartType}
          />

          {getChartDescription(item)}
          {
              item.genState === 'error' ? (
                  <div style={{ textAlign: 'center', color: '#ff4d4f' }}>
                    <div style={{ fontSize: 20 }}>⚠️</div>
                    <div>图表生成失败</div>
                  </div>
              ) : (['waiting', 'generating'].includes(item.genState || '') ? (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Spin tip={item.genState === 'generating' ? '图表生成中...' : '等待生成...'} size="large">
                    <div style={{ width: 272, height: 200 }} />
                  </Spin>
                </div>
              ) : (
                item.genChart ? (
                  <EChartsReact option={JSON.parse(item.genChart)} />
                ) : (
                  <img
                    width={272}
                    height={200}
                    alt="logo"
                    src="https://jfcd2024.oss-cn-shanghai.aliyuncs.com/pic.jpg"
                  />
                )
              ))
          }

          <div style={{textAlign: 'center'}}>
            <IconText icon={FieldTimeOutlined} text={toReadableDate(item.createTime || '')} />
          </div>
        </List.Item>
      </Card>
      </Popover>
    )}
  />
  </div>
  );
};

export default Login;
