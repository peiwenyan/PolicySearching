from sklearn.feature_extraction.text import TfidfVectorizer  # 词袋模型
from sklearn.naive_bayes import MultinomialNB  # ,BernoulliNB,GaussianNB
from sklearn import metrics  # 评价函数（即使是深度学习，也会使用评价方法）
import joblib  # 打包模型 # import pickle # 打包模型

from get_cut_word import load_data, load_stops

"""
1.完成分词
(1)加载停用词
(2)读取训练数据
(3)文本向量化：把读取的文本转换成对应的数字表示：（TF-IDF，BOW，N-gram）
"""
# 加载停用词
stop_words = load_stops('data/stop_words/stopword.txt')
# 读取训练数据
train_datas, train_lables = load_data('data/train_data')
# 文本向量化：把读取的文本转换成对应的数字表示：（TF-IDF，BOW，N-gram）
count_vec = TfidfVectorizer(stop_words=stop_words)
# {train_datas：文本，train_feature:文本对应的向量表示}
train_feature = count_vec.fit_transform(train_datas)  # 把文本转换成数字
print(len(train_datas))
print(len(train_lables))
print(train_feature.shape)

"""
2.模型的训练、测试和保存
"""
# 模型的构建和训练：sklearn中的bayes:MultinomialNB（先验为多项式分布的朴素贝叶斯）
# alpha：浮点型可选参数，默认为1.0，拉普拉斯平滑系数
clf = MultinomialNB(alpha=0.5).fit(train_feature, train_lables)
# 读取测试数据，同时需要将测试文本转换成对应的向量表示
test_datas, test_labels = load_data('data/test_data/')
test_feature = count_vec.transform(test_datas)
# 预测和评价
predict_labels = clf.predict(test_feature)  # 模型的预测
# clf.predict_proba() # 返回每个预测的概率
score = metrics.accuracy_score(test_labels, predict_labels)
print(score)
# 模型的保存
joblib.dump(clf, 'bayes.pkl')
joblib.dump(count_vec, 'count_vec.pkl')
