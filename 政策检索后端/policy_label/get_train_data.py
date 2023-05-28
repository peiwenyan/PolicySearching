import pandas as pd


def read(filepath):
    # 读取政策分类文件，获取相关列
    file = pd.read_excel(filepath)
    content = file['政策标题']
    label = file['分类1']
    label = [str(k).strip(' ') for k in label]
    # 删除的分类
    label_delete = ['nan', '党建', '党建设', '社会', '渔业', '信息化', '信息安全', '汽车', '体育', '违法犯罪', '行业', '武汉', '  农业', '食品安全', '政府',
                    '民生']
    # 分类合并处理后的分类结果
    label_result = ['建筑', '招标', '农业', '文化', '交通/物流/供应链', '节能环保', '税务', '服务业', '旅游', '法律', '教育', '工业', '经济', '金融贸易',
                    '科学技术', '政务', '社会保障', '医疗健康', '其他']
    content_len = len(content)
    # 将每一行转换为分类对应的数字，并依次存入列表，最终写入txt文件
    result = []
    l = 0
    for i in range(content_len):
        # 减少其他在训练数据中的出现次数(l记录次数)
        if label[i] == '其他 ':
            label[i] = '其他'
        if label[i] == '其他' and content_len % 2 == 1:
            l = l + 1
            continue
        # 对一些分类进行合并处理
        if label[i] == '销售' or label[i] == '投资':
            label[i] = '金融贸易'
        if label[i] == '环境' or label[i] == '节能减排':
            label[i] = '节能环保'
        if label[i] == '交通' or label[i] == '物流' or label[i] == '供应链':
            label[i] = '交通/物流/供应链'
        if label[i] == '科技' or label[i] == '互联网':
            label[i] = '科学技术'
        if label[i] == '行政' or label[i] == '财政':
            label[i] = '政务'
        if label[i] == '低保' or label[i] == '脱贫' or label[i] == '社保' or label[i] == '扶贫':
            label[i] = '社会保障'
        # 删除某些分类，将其他分类进行处理后存储
        if label[i] not in label_delete:
            label_num = label_result.index(label[i]) + 1
            result.append(content[i] + '\t' + str(label_num) + '\n')
    # 输出其他分类的出现次数和当前合并处理后的分类列表
    # print(l)
    # print(set(label_result))
    # d = {}
    # for key in label_result:
    #     d[key] = d.get(key, 0) + 1
    # print(d)
    r = open('data/train_data/train.txt', mode='a+', encoding='utf-8')
    r.writelines(result)


if __name__ == '__main__':
    read("data/excel/word-results2.0.xlsx")
    # {'税务': 85, '政务': 56, '金融贸易': 101, '节能环保': 30, '医疗健康': 38, '科学技术': 34, '教育': 79, '工业': 47, '建筑': 21,
    # '社会保障': 552, '交通/物流/供应链': 74, '招标': 33, '服务业': 13, '农业': 25, '经济': 35, '旅游': 13, '法律': 12, '文化': 12}
    read("data/excel/标注.xlsx")
    # {'其他': 399, '经济': 18, '工业': 13, '社会保障': 380, '税务': 48, '招标': 26, '政务': 17, '教育': 34, '医疗健康': 17, '金融贸易': 28,
    # '服务业': 7, '旅游': 7, '建筑': 8, '交通/物流/供应链': 35, '法律': 5, '节能环保': 10, '科学技术': 17, '农业': 10, '文化': 4}
    read("data/excel/标注检查.xlsx")
    # {'医疗健康': 447, '金融贸易': 499, '农业': 283, '服务业': 164, '教育': 178, '法律': 98, '节能环保': 189, '文化': 102, '科学技术': 192,
    # '工业': 417, '建筑': 121, '交通/物流/供应链': 229, '旅游': 28}
