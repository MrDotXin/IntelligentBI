import access from "@/access";

export default [
  {
    path: '/user', name:"用户", layout: false,
    routes: [
      {
        path: '/user/login',
        component: './User/Login',
        access: 'notLogin'
      },
      {
        path: '/user/register',
        component: './User/Register',
        access: 'notLogin'
      }
    ]
  },
  { path: '/welcome', name:"欢迎", icon: 'smile', component: './Welcome' },
  {
    path: '/chart',
    name:"图表",
    icon: 'AreaChartOutlined',
    routes:[
      { path: '/chart/add', name:"生成图表", icon: 'AppstoreAddOutlined', component: './Chart/AddChart' },
      { path: '/chart/my/chart', name:"查看我的图表", icon: 'DotChartOutlined', component: './Chart/MyChart' },
    ]
  },
  {
    path: '/admin',
    name:"管理员",
    icon: 'crown',
    access: 'canAdmin',
    routes: [
      { path: '/admin/sub', name:"管理员页面", redirect: '/admin/sub-page' },
      { path: '/admin/sub-page', name:"二级管理页", component: './Admin' },
    ],
  },
  { path: '/', redirect: '/welcome' },
  { path: '*', layout: false, component: './404' },
];
