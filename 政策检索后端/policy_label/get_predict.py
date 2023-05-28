import joblib
import jieba
import pandas as pd
import xlwt

dic_class = ['建筑', '招标', '农业', '文化', '交通/物流/供应链', '节能环保', '税务', '服务业', '旅游', '法律', '教育', '工业', '经济', '金融贸易',
             '科学技术', '政务', '社会保障', '医疗健康', '其他']


def load_model(model_path, count_vec_path):
    """
     保存模型用dump，加载模型用load
    """
    model = joblib.load(model_path)
    count_vec = joblib.load(count_vec_path)

    return model, count_vec


def nb_predict(sentence, model, count_vec):
    # 1. 将句子按照空格完成分词
    words = jieba.cut(sentence)
    s = ' '.join(words)
    # 2. 把要预测的句子转换成对应的向量表示
    predict_feature = count_vec.transform([s])
    predict_label = model.predict(predict_feature)  # 预测
    place = int(predict_label[0]) - 1
    return dic_class[place]


def predict():
    model, count_vec = load_model('bayes.pkl', 'count_vec.pkl')
    predict_file = pd.read_excel('data/predict_data/policyinfo.xlsx')
    predict_file = pd.DataFrame(predict_file)
    pd.set_option('expand_frame_repr', False)

    predict_id = predict_file['POLICY_ID']
    predict_content = predict_file['POLICY_TITLE']
    predict_detail = predict_file['POLICY_BODY']
    predict_len = len(predict_id)
    label_result = []
    for i in range(predict_len):
        label = nb_predict(predict_content[i] + str(predict_detail[i]), model, count_vec)
        label_result.append(label)
        print(str(i) + '  ' + label + '\t' + predict_content[i] + '\n')
        # result.append(str(predict_id[i])+'\t'+predict_content[i]+'\t'+result.xlsx+'\n')
    # r = open('result.txt', mode='w+', encoding='utf-8')
    # r.writelines(label_result)
    print(len(predict_file))
    print(len(label_result))

    predict_file['LABEL'] = label_result
    predict_file.to_excel("result.xlsx")


predict()
