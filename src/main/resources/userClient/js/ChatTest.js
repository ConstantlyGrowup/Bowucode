document.addEventListener("DOMContentLoaded", () => {
    const chatMessages = document.getElementById("chat-messages");
    const chatForm = document.getElementById("chat-form");
    const userInput = document.getElementById("user-input");

    // 存储对话历史
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
            const baseUrl = '/api/ai/chat';
            const params = new URLSearchParams({
                prompt: message,
                messages: JSON.stringify(messageHistory) // 发送整个聊天历史
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
                const data = event.data.replace('data:', ''); // 移除可能存在的"data:"前缀
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
                    messageText.textContent = receivedText; // 更新DOM
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
        avatarImg.alt = "Bot Avatar";
        avatarImg.classList.add("bot-avatar");

        const messageText = document.createElement("div");
        messageText.classList.add("message-text");
        messageText.textContent = message;

        messageElement.appendChild(avatarImg);
        messageElement.appendChild(messageText);

        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
});
