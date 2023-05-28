import jieba
import os


# 1. 加载停用词：读取停用词文件

def load_stops(file_path):
    """
        1. 打开文件，需要指定文件的编码格式（utf-8）
        2. 按行读取文件中的每隔单词：readlines
        3. 遍历文件中的每个单词：把单词存放到一个新建的列表中
        4. 返回停用词列表
    """
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    words = []
    # 3.  遍历文件中的每个单词
    for line in lines:
        line = line.strip('\n')
        words.append(line)
    return words


words = load_stops('data/stop_words/stopword.txt')
print(len(words))


# 2. 中文分词具体实现
def load_file(file_path):
    """
    1. 打开并读取文件 open()
    2. 把文件中的内容保存的内存变量lines中
    3. 变量文件中的内容
    4. 获取标签和对应的文本数据
    5. 把文本数据按空格进行分词
    6. 把分词后的文本和标签给返回
    """
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    titles = []  # 文本的内容
    labels = []  # 标签的内容
    # 3. 遍历文件
    for line in lines:
        line = line.encode('unicode-escape').decode('unicode-escape')
        line = line.strip().rstrip('\n')

        # 4. 获取标签和对应的文本数据
        _line = line.split("\t")
        if len(_line) != 2:
            continue
        title, label = _line  # 获取到没行文本中的标签和文本
        # 5. 把文本数据按空格进行分词
        words = jieba.cut(title)  # 返回值是generator
        words = " ".join(words)
        words = words.strip()

        # 6. 把分词后的文本和标签给返回
        titles.append(words)
        labels.append(label)

    return titles, labels


# 3. 读取训练集数据以实现训练集分词
def load_data(_dir):
    """
    1. os模块读取文件夹下面所有的文件
    2. 遍历文件夹中所有的文件
    3. 调用分词函数完成所有数据的加载
    """
    # 1. os模块读取文件夹下面所有的文件
    file_list = os.listdir(_dir)

    titles_list = []
    labels_list = []
    # 2. 遍历文件夹中所有的文件
    for file_name in file_list:
        file_path = _dir + '/' + file_name
        print(file_path)
        # 3. 调用分词函数完成所有数据的加载
        titles, labels = load_file(file_path)
        titles_list += titles
        labels_list += labels

    return titles_list, labels_list
