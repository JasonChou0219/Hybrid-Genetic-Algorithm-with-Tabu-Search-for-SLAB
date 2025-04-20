#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
problemFormulationToLatex.py

这个脚本用于将 Problem_Formulation.txt 文件中的所有 ¥ 符号替换成 $ 符号。
这个转换对于将普通文本转换为 LaTeX 格式很有用，因为在 LaTeX 中，$ 符号用于标记数学表达式。
"""

import os
import sys


def convert_file(file_path):
    """
    读取指定的文件，将所有 ¥ 符号替换为 $，然后保存回原文件。

    参数:
        file_path (str): 要处理的文件路径

    返回:
        bool: 操作成功返回 True，否则返回 False
    """
    try:
        # 检查文件是否存在
        if not os.path.exists(file_path):
            print(f"错误: 文件 '{file_path}' 不存在")
            return False

        # 读取文件内容
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # 替换 ¥ 为 $
        new_content = content.replace('¥', '$')

        # 计算替换次数
        replace_count = content.count('¥')

        # 写回文件
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)

        print(f"成功处理文件 '{file_path}'")
        print(f"共替换了 {replace_count} 个 ¥ 符号为 $ 符号")
        return True

    except Exception as e:
        print(f"处理文件时出错: {str(e)}")
        return False


def main():
    """主函数，处理命令行参数并执行转换"""
    # 默认处理当前目录下的 Problem_Formulation.txt
    default_file = 'Problem_Formulation.txt'

    # 获取命令行参数（如果有）
    if len(sys.argv) > 1:
        file_path = sys.argv[1]
    else:
        file_path = default_file

    print(f"正在处理文件: {file_path}")
    success = convert_file(file_path)

    if success:
        print("处理完成!")
    else:
        print("处理失败!")
        sys.exit(1)


if __name__ == "__main__":
    main()