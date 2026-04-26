// 页面跳转函数
function navigateTo(page) {
    window.location.href = page;
}

// 登录功能
function login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    // 简单的登录验证
    if (username && password) {
        // 模拟登录成功，跳转到首页
        navigateTo('index.html');
    } else {
        alert('请输入用户名和密码');
    }
}

// 表单提交功能
function submitForm(formId) {
    const form = document.getElementById(formId);
    // 模拟表单提交
    alert('表单提交成功');
    form.reset();
    // 跳转到列表页面
    if (formId === 'case-form') {
        navigateTo('case-management.html');
    } else if (formId === 'user-form') {
        navigateTo('user-management.html');
    }
}

// 确认删除功能
function confirmDelete(itemType) {
    if (confirm(`确定要删除该${itemType}吗？`)) {
        alert(`${itemType}删除成功`);
    }
}

// 上传文件功能
function uploadFile() {
    // 模拟文件上传
    alert('文件上传成功');
}

// 发起检测功能
function startDetection() {
    // 模拟检测过程
    alert('检测任务已开始，正在处理...');
    setTimeout(() => {
        navigateTo('detection-result.html');
    }, 1000);
}

// 导出结果功能
function exportResult() {
    // 模拟导出过程
    alert('结果导出成功，正在生成下载链接...');
    setTimeout(() => {
        alert('下载链接已生成，请点击下载');
    }, 1000);
}

// 切换模型功能
function switchModel(modelId) {
    // 模拟模型切换
    alert(`模型 ${modelId} 切换成功`);
}

// 人工反馈功能
function submitFeedback() {
    // 模拟反馈提交
    alert('反馈提交成功');
}

// 模态框功能
function openModal(modalId) {
    document.getElementById(modalId).style.display = 'block';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

// 点击模态框外部关闭
window.onclick = function(event) {
    const modals = document.getElementsByClassName('modal');
    for (let i = 0; i < modals.length; i++) {
        if (event.target == modals[i]) {
            modals[i].style.display = 'none';
        }
    }
}

// 密码强度检测
function checkPasswordStrength() {
    const password = document.getElementById('password').value;
    const strengthIndicator = document.getElementById('password-strength');
    
    let strength = 0;
    if (password.length >= 8) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;
    
    switch(strength) {
        case 0:
            strengthIndicator.textContent = '请输入密码';
            strengthIndicator.style.color = '#757575';
            break;
        case 1:
        case 2:
            strengthIndicator.textContent = '密码强度：弱';
            strengthIndicator.style.color = '#f44336';
            break;
        case 3:
            strengthIndicator.textContent = '密码强度：中';
            strengthIndicator.style.color = '#ff9800';
            break;
        case 4:
        case 5:
            strengthIndicator.textContent = '密码强度：强';
            strengthIndicator.style.color = '#4caf50';
            break;
    }
}

// 初始化页面
function initPage() {
    // 检查当前页面
    const currentPage = window.location.pathname.split('/').pop();
    
    // 高亮当前菜单
    const menuItems = document.querySelectorAll('.menu-item');
    menuItems.forEach(item => {
        const href = item.getAttribute('href');
        if (href === currentPage) {
            item.classList.add('active');
        }
    });
    
    // 绑定事件
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();
            login();
        });
    }
    
    const passwordInput = document.getElementById('password');
    if (passwordInput) {
        passwordInput.addEventListener('input', checkPasswordStrength);
    }
}

// 页面加载完成后初始化
window.onload = initPage;
