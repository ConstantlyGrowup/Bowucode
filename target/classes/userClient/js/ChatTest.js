import { marked } from 'https://cdn.jsdelivr.net/npm/marked@4.3.0/lib/marked.esm.min.js';
// 配置 marked 的选项,处理换行
marked.setOptions({
    breaks: true // 将换行符 (\n) 转换为 <br> 标签
});
document.addEventListener("DOMContentLoaded", () => {
    const chatMessages = document.getElementById("chat-messages");
    const chatForm = document.getElementById("chat-form");
    const userInput = document.getElementById("user-input");

    // 生成UUID函数（简化版）
    function generateUUID() {
        let d = new Date().getTime();
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            let r = (d + Math.random()*16)%16 | 0;
            d = Math.floor(d/16);
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
    }

    // 存储对话历史和sessionId
    let sessionId = generateUUID(); // 创建一次性的sessionId
    const messageHistory = [{
        role: "assistant",
        content: "欢迎来到古董展览馆。我是您的导览僧，很高兴为您介绍我们珍贵的文物收藏。请问您对哪个朝代或者哪类文物最感兴趣？"
    }];

    // 初始欢迎消息
     addBotMessage(messageHistory[0].content);

    // 修改后的submit事件监听
    chatForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const message = userInput.value.trim();
        if (message) {
            messageHistory.push({
                role: "user",
                content: message
            });

            addUserMessage(message);
            userInput.value = "";
            userInput.disabled = true;

            // 构造请求URL
            const baseUrl = '/api/ai/chat'; // 注意这里应该与后端配置的路径一致
            const params = new URLSearchParams({
                prompt: message,
                sessionId: sessionId
            });
            const url = `${baseUrl}?${params}`;

            // 创建EventSource连接
            const eventSource = new EventSource(url);
            const messageElement = document.createElement("div");
            messageElement.classList.add("message", "bot-message");

            const avatarImg = document.createElement("img");
            avatarImg.src = "monkTang.svg";
            avatarImg.alt = "Bot Avatar";
            avatarImg.classList.add("bot-avatar");

            const messageText = document.createElement("div");
            messageText.classList.add("message-text");
            messageText.textContent = "";

            messageElement.appendChild(avatarImg);
            messageElement.appendChild(messageText);
            chatMessages.appendChild(messageElement);

            let receivedText = ''; // 用于累积接收的文本

            eventSource.onmessage = (event) => {
                const data = event.data; // 这里假设服务器不会发送"data:"前缀的数据
                if (data === "[ovo-done]") {
                    // 收到结束标志，将累积的文本作为助手的消息添加到对话历史中
                    if (receivedText.trim()) { // 确保不是空消息
                        messageHistory.push({
                            role: "assistant",
                            content: receivedText
                        });
                    }
                    eventSource.close(); // 关闭连接
                    userInput.disabled = false;
                } else {
                    receivedText += data; // 累积接收到的数据

                    // 动态解析 Markdown 并更新 DOM
                    const htmlContent = marked.parse(receivedText);
                    messageText.innerHTML = htmlContent;

                    chatMessages.scrollTop = chatMessages.scrollHeight;
                }
            };

            eventSource.onerror = () => {
                eventSource.close(); // 出错时仅关闭连接，不显示任何提示
                userInput.disabled = false;
            };
        }
    });

    function addUserMessage(message) {
        const messageElement = document.createElement("div");
        messageElement.classList.add("message", "user-message");

        const messageText = document.createElement("div");
        messageText.classList.add("message-text");
        messageText.textContent = message;

        const avatarImg = document.createElement("img");
        avatarImg.alt = "User Avatar";
        avatarImg.classList.add("user-avatar");

        messageElement.appendChild(messageText);
        messageElement.appendChild(avatarImg);

        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function addBotMessage(message) {
        const messageElement = document.createElement("div");
        messageElement.classList.add("message", "bot-message");

        const avatarImg = document.createElement("img");
         avatarImg.src = "img/monkTang.svg";
        avatarImg.alt = "Bot Avatar";
        avatarImg.classList.add("bot-avatar");

        const messageText = document.createElement("div");
        messageText.classList.add("message-text");

        // 将 Markdown 转换为 HTML
        let htmlContent = marked.parse(message);

        // 手动处理换行符
        htmlContent = htmlContent.replace(/\n/g, '<br>');

        messageText.innerHTML = htmlContent; // 使用 innerHTML 插入 HTML 内容

        messageElement.appendChild(avatarImg);
        messageElement.appendChild(messageText);

        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
});